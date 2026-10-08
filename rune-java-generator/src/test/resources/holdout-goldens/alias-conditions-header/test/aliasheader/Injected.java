package test.aliasheader;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.aliasheader.meta.InjectedMeta;

import static java.util.Optional.ofNullable;

/**
 * attributes wired to a condition class named Inject.
 * @version 0.0.0.test
 */
@RosettaDataType(value="Injected", builder=Injected.InjectedBuilderImpl.class, version="0.0.0.test")
@RuneDataType(value="Injected", model="test", builder=Injected.InjectedBuilderImpl.class, version="0.0.0.test")
public interface Injected extends RosettaModelObject {

	InjectedMeta metaData = new InjectedMeta();

	/*********************** Getter Methods  ***********************/
	Integer getInj();
	List<Integer> getInjs();

	/*********************** Build Methods  ***********************/
	Injected build();
	
	Injected.InjectedBuilder toBuilder();
	
	static Injected.InjectedBuilder builder() {
		return new Injected.InjectedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Injected> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Injected> getType() {
		return Injected.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("inj"), Integer.class, getInj(), this);
		processor.processBasic(path.newSubPath("injs"), Integer.class, getInjs(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface InjectedBuilder extends Injected, RosettaModelObjectBuilder {
		Injected.InjectedBuilder setInj(Integer inj);
		Injected.InjectedBuilder addInjs(Integer injs);
		Injected.InjectedBuilder addInjs(Integer injs, int idx);
		Injected.InjectedBuilder addInjs(List<Integer> injs);
		Injected.InjectedBuilder setInjs(List<Integer> injs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("inj"), Integer.class, getInj(), this);
			processor.processBasic(path.newSubPath("injs"), Integer.class, getInjs(), this);
		}
		

		Injected.InjectedBuilder prune();
	}

	/*********************** Immutable Implementation of Injected  ***********************/
	class InjectedImpl implements Injected {
		private final Integer inj;
		private final List<Integer> injs;
		
		protected InjectedImpl(Injected.InjectedBuilder builder) {
			this.inj = builder.getInj();
			this.injs = ofNullable(builder.getInjs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("inj")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("inj")
		public Integer getInj() {
			return inj;
		}
		
		@Override
		@RosettaAttribute("injs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("injs")
		public List<Integer> getInjs() {
			return injs;
		}
		
		@Override
		public Injected build() {
			return this;
		}
		
		@Override
		public Injected.InjectedBuilder toBuilder() {
			Injected.InjectedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Injected.InjectedBuilder builder) {
			ofNullable(getInj()).ifPresent(builder::setInj);
			ofNullable(getInjs()).ifPresent(builder::setInjs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Injected _that = getType().cast(o);
		
			if (!Objects.equals(inj, _that.getInj())) return false;
			if (!ListEquals.listEquals(injs, _that.getInjs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (inj != null ? inj.hashCode() : 0);
			_result = 31 * _result + (injs != null ? injs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Injected {" +
				"inj=" + this.inj + ", " +
				"injs=" + this.injs +
			'}';
		}
	}

	/*********************** Builder Implementation of Injected  ***********************/
	class InjectedBuilderImpl implements Injected.InjectedBuilder {
	
		protected Integer inj;
		protected List<Integer> injs = new ArrayList<>();
		
		@Override
		@RosettaAttribute("inj")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("inj")
		public Integer getInj() {
			return inj;
		}
		
		@Override
		@RosettaAttribute("injs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("injs")
		public List<Integer> getInjs() {
			return injs;
		}
		
		@RosettaAttribute("inj")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("inj")
		@Override
		public Injected.InjectedBuilder setInj(Integer _inj) {
			this.inj = _inj == null ? null : _inj;
			return this;
		}
		
		@RosettaAttribute("injs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("injs")
		@Override
		public Injected.InjectedBuilder addInjs(Integer _injs) {
			if (_injs != null) {
				this.injs.add(_injs);
			}
			return this;
		}
		
		@Override
		public Injected.InjectedBuilder addInjs(Integer _injs, int idx) {
			getIndex(this.injs, idx, () -> _injs);
			return this;
		}
		
		@Override
		public Injected.InjectedBuilder addInjs(List<Integer> injss) {
			if (injss != null) {
				for (final Integer toAdd : injss) {
					this.injs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("injs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("injs")
		@Override
		public Injected.InjectedBuilder setInjs(List<Integer> injss) {
			if (injss == null) {
				this.injs = new ArrayList<>();
			} else {
				this.injs = injss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Injected build() {
			return new Injected.InjectedImpl(this);
		}
		
		@Override
		public Injected.InjectedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Injected.InjectedBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getInj()!=null) return true;
			if (getInjs()!=null && !getInjs().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Injected.InjectedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Injected.InjectedBuilder o = (Injected.InjectedBuilder) other;
			
			
			merger.mergeBasic(getInj(), o.getInj(), this::setInj);
			merger.mergeBasic(getInjs(), o.getInjs(), (Consumer<Integer>) this::addInjs);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Injected _that = getType().cast(o);
		
			if (!Objects.equals(inj, _that.getInj())) return false;
			if (!ListEquals.listEquals(injs, _that.getInjs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (inj != null ? inj.hashCode() : 0);
			_result = 31 * _result + (injs != null ? injs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "InjectedBuilder {" +
				"inj=" + this.inj + ", " +
				"injs=" + this.injs +
			'}';
		}
	}
}
