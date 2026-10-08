package chaos.s25.a5crlf;

import chaos.s25.a5crlf.meta.C25OptAMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Choice option A.
 * @version 1.0.0
 */
@RosettaDataType(value="C25OptA", builder=C25OptA.C25OptABuilderImpl.class, version="1.0.0")
@RuneDataType(value="C25OptA", model="chaos", builder=C25OptA.C25OptABuilderImpl.class, version="1.0.0")
public interface C25OptA extends RosettaModelObject {

	C25OptAMeta metaData = new C25OptAMeta();

	/*********************** Getter Methods  ***********************/
	String getPa();
	String getShared();

	/*********************** Build Methods  ***********************/
	C25OptA build();
	
	C25OptA.C25OptABuilder toBuilder();
	
	static C25OptA.C25OptABuilder builder() {
		return new C25OptA.C25OptABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C25OptA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C25OptA> getType() {
		return C25OptA.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("pa"), String.class, getPa(), this);
		processor.processBasic(path.newSubPath("shared"), String.class, getShared(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C25OptABuilder extends C25OptA, RosettaModelObjectBuilder {
		C25OptA.C25OptABuilder setPa(String pa);
		C25OptA.C25OptABuilder setShared(String shared);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("pa"), String.class, getPa(), this);
			processor.processBasic(path.newSubPath("shared"), String.class, getShared(), this);
		}
		

		C25OptA.C25OptABuilder prune();
	}

	/*********************** Immutable Implementation of C25OptA  ***********************/
	class C25OptAImpl implements C25OptA {
		private final String pa;
		private final String shared;
		
		protected C25OptAImpl(C25OptA.C25OptABuilder builder) {
			this.pa = builder.getPa();
			this.shared = builder.getShared();
		}
		
		@Override
		@RosettaAttribute("pa")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pa")
		public String getPa() {
			return pa;
		}
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public String getShared() {
			return shared;
		}
		
		@Override
		public C25OptA build() {
			return this;
		}
		
		@Override
		public C25OptA.C25OptABuilder toBuilder() {
			C25OptA.C25OptABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C25OptA.C25OptABuilder builder) {
			ofNullable(getPa()).ifPresent(builder::setPa);
			ofNullable(getShared()).ifPresent(builder::setShared);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25OptA _that = getType().cast(o);
		
			if (!Objects.equals(pa, _that.getPa())) return false;
			if (!Objects.equals(shared, _that.getShared())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pa != null ? pa.hashCode() : 0);
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25OptA {" +
				"pa=" + this.pa + ", " +
				"shared=" + this.shared +
			'}';
		}
	}

	/*********************** Builder Implementation of C25OptA  ***********************/
	class C25OptABuilderImpl implements C25OptA.C25OptABuilder {
	
		protected String pa;
		protected String shared;
		
		@Override
		@RosettaAttribute("pa")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("pa")
		public String getPa() {
			return pa;
		}
		
		@Override
		@RosettaAttribute("shared")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("shared")
		public String getShared() {
			return shared;
		}
		
		@RosettaAttribute("pa")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("pa")
		@Override
		public C25OptA.C25OptABuilder setPa(String _pa) {
			this.pa = _pa == null ? null : _pa;
			return this;
		}
		
		@RosettaAttribute("shared")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("shared")
		@Override
		public C25OptA.C25OptABuilder setShared(String _shared) {
			this.shared = _shared == null ? null : _shared;
			return this;
		}
		
		@Override
		public C25OptA build() {
			return new C25OptA.C25OptAImpl(this);
		}
		
		@Override
		public C25OptA.C25OptABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25OptA.C25OptABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPa()!=null) return true;
			if (getShared()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25OptA.C25OptABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C25OptA.C25OptABuilder o = (C25OptA.C25OptABuilder) other;
			
			
			merger.mergeBasic(getPa(), o.getPa(), this::setPa);
			merger.mergeBasic(getShared(), o.getShared(), this::setShared);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25OptA _that = getType().cast(o);
		
			if (!Objects.equals(pa, _that.getPa())) return false;
			if (!Objects.equals(shared, _that.getShared())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pa != null ? pa.hashCode() : 0);
			_result = 31 * _result + (shared != null ? shared.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25OptABuilder {" +
				"pa=" + this.pa + ", " +
				"shared=" + this.shared +
			'}';
		}
	}
}
