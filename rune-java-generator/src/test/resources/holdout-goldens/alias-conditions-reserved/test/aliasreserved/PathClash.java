package test.aliasreserved;

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
import test.aliasreserved.meta.PathClashMeta;

import static java.util.Optional.ofNullable;

/**
 * An attribute typed through the alias whose condition class is Path - the injected field wants the name path.
 * @version 0.0.0
 */
@RosettaDataType(value="PathClash", builder=PathClash.PathClashBuilderImpl.class, version="0.0.0")
@RuneDataType(value="PathClash", model="test", builder=PathClash.PathClashBuilderImpl.class, version="0.0.0")
public interface PathClash extends RosettaModelObject {

	PathClashMeta metaData = new PathClashMeta();

	/*********************** Getter Methods  ***********************/
	Integer getPa();
	List<Integer> getPas();

	/*********************** Build Methods  ***********************/
	PathClash build();
	
	PathClash.PathClashBuilder toBuilder();
	
	static PathClash.PathClashBuilder builder() {
		return new PathClash.PathClashBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends PathClash> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends PathClash> getType() {
		return PathClash.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("pa"), Integer.class, getPa(), this);
		processor.processBasic(path.newSubPath("pas"), Integer.class, getPas(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface PathClashBuilder extends PathClash, RosettaModelObjectBuilder {
		PathClash.PathClashBuilder setPa(Integer pa);
		PathClash.PathClashBuilder addPas(Integer pas);
		PathClash.PathClashBuilder addPas(Integer pas, int idx);
		PathClash.PathClashBuilder addPas(List<Integer> pas);
		PathClash.PathClashBuilder setPas(List<Integer> pas);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("pa"), Integer.class, getPa(), this);
			processor.processBasic(path.newSubPath("pas"), Integer.class, getPas(), this);
		}
		

		PathClash.PathClashBuilder prune();
	}

	/*********************** Immutable Implementation of PathClash  ***********************/
	class PathClashImpl implements PathClash {
		private final Integer pa;
		private final List<Integer> pas;
		
		protected PathClashImpl(PathClash.PathClashBuilder builder) {
			this.pa = builder.getPa();
			this.pas = ofNullable(builder.getPas()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("pa")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pa")
		public Integer getPa() {
			return pa;
		}
		
		@Override
		@RosettaAttribute("pas")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("pas")
		public List<Integer> getPas() {
			return pas;
		}
		
		@Override
		public PathClash build() {
			return this;
		}
		
		@Override
		public PathClash.PathClashBuilder toBuilder() {
			PathClash.PathClashBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(PathClash.PathClashBuilder builder) {
			ofNullable(getPa()).ifPresent(builder::setPa);
			ofNullable(getPas()).ifPresent(builder::setPas);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PathClash _that = getType().cast(o);
		
			if (!Objects.equals(pa, _that.getPa())) return false;
			if (!ListEquals.listEquals(pas, _that.getPas())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pa != null ? pa.hashCode() : 0);
			_result = 31 * _result + (pas != null ? pas.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PathClash {" +
				"pa=" + this.pa + ", " +
				"pas=" + this.pas +
			'}';
		}
	}

	/*********************** Builder Implementation of PathClash  ***********************/
	class PathClashBuilderImpl implements PathClash.PathClashBuilder {
	
		protected Integer pa;
		protected List<Integer> pas = new ArrayList<>();
		
		@Override
		@RosettaAttribute("pa")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pa")
		public Integer getPa() {
			return pa;
		}
		
		@Override
		@RosettaAttribute("pas")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("pas")
		public List<Integer> getPas() {
			return pas;
		}
		
		@RosettaAttribute("pa")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pa")
		@Override
		public PathClash.PathClashBuilder setPa(Integer _pa) {
			this.pa = _pa == null ? null : _pa;
			return this;
		}
		
		@RosettaAttribute("pas")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("pas")
		@Override
		public PathClash.PathClashBuilder addPas(Integer _pas) {
			if (_pas != null) {
				this.pas.add(_pas);
			}
			return this;
		}
		
		@Override
		public PathClash.PathClashBuilder addPas(Integer _pas, int idx) {
			getIndex(this.pas, idx, () -> _pas);
			return this;
		}
		
		@Override
		public PathClash.PathClashBuilder addPas(List<Integer> pass) {
			if (pass != null) {
				for (final Integer toAdd : pass) {
					this.pas.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("pas")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("pas")
		@Override
		public PathClash.PathClashBuilder setPas(List<Integer> pass) {
			if (pass == null) {
				this.pas = new ArrayList<>();
			} else {
				this.pas = pass.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public PathClash build() {
			return new PathClash.PathClashImpl(this);
		}
		
		@Override
		public PathClash.PathClashBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PathClash.PathClashBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPa()!=null) return true;
			if (getPas()!=null && !getPas().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PathClash.PathClashBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			PathClash.PathClashBuilder o = (PathClash.PathClashBuilder) other;
			
			
			merger.mergeBasic(getPa(), o.getPa(), this::setPa);
			merger.mergeBasic(getPas(), o.getPas(), (Consumer<Integer>) this::addPas);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PathClash _that = getType().cast(o);
		
			if (!Objects.equals(pa, _that.getPa())) return false;
			if (!ListEquals.listEquals(pas, _that.getPas())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pa != null ? pa.hashCode() : 0);
			_result = 31 * _result + (pas != null ? pas.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PathClashBuilder {" +
				"pa=" + this.pa + ", " +
				"pas=" + this.pas +
			'}';
		}
	}
}
